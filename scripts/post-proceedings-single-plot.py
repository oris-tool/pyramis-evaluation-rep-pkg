
import os
import pandas as pd
import matplotlib.pyplot as plt
import argparse
import re
import glob
from pathlib import Path

def load_cdf_data(file_path):
    """
    Load CDF data from file using pandas with whitespace delimiter.
    
    Args:
        file_path (str): Path to the data file
        
    Returns:
        pandas.DataFrame: DataFrame with data, or None if loading fails
    """
    try:
        # Use the original method with delim_whitespace=True
        df = pd.read_csv(file_path, delim_whitespace=True, header=None)
        
        # If we have at least 2 columns, name them 'x' and 'y'
        if df.shape[1] >= 2:
            df.columns = ['x', 'y'] + [f'col_{i}' for i in range(2, df.shape[1])]
            return df
        else:
            print(f"Warning: File {file_path} does not have enough columns")
            return None
            
    except Exception as e:
        print(f"Error loading {file_path}: {str(e)}")
        return None

def get_curve_style(filename):
    if 'GT' in filename:
        return {'color': 'black', 'linestyle': '--', 'linewidth': 2}  # nero tratteggiato
    elif 'ts0.1' and 'pyramisCDF' in filename:
        return {'color': '#0077BB', 'linewidth': 2}     # blu intenso
    elif 'simulationCDF' in filename:
        return {'color': '#CC3311', 'linewidth': 2}     # rosso
    elif 'ts0.1' in filename:
        return {'color': '#0077BB', 'linewidth': 2}     # blu intenso
    elif 'ts0.2' in filename:
        return {'color': '#009988', 'linewidth': 2}     # verde acqua
    elif 'ts0.4' in filename:
        return {'color': '#CC3311', 'linewidth': 2}     # rosso
    else:
        return {'color': '#CC3311', 'linewidth': 2}     # rosso


def get_curve_label(filename):
    patterns = {
        r'.*GT.*': 'Ground truth',
        r'^pyramisCDF.*?_ts(\d+(?:\.\d+)?)\.txt$': lambda m: f'Pyramis - time tick {m.group(1)}',
        r'^simulationCDF.*?_ts(\d+(?:\.\d+)?)\.txt$': lambda m: f'Simulation - time tick {m.group(1)}',
    }
    
    for pattern, label_func in patterns.items():
        match = re.match(pattern, filename)
        if match:
            if callable(label_func):
                return label_func(match)
            else:
                return label_func
    
    return filename

def plot_cdfs_from_folder(folder, gt_path):
    """
    Plot CDFs from simulationCDF and pyramisCDF files in folder, plus GT file from separate location.
    
    Args:
        folder (str): Path to the folder containing simulationCDF and pyramisCDF files
        gt_path (str): Path to the GT file (can be in a different location)
    """
    
    # Convert to Path objects for safer handling
    folder_path = Path(folder)
    gt_file_path = Path(gt_path)
    
    # Validate inputs
    if not folder_path.exists():
        print(f"Error: Folder does not exist: {folder}")
        return
    
    if not folder_path.is_dir():
        print(f"Error: Path is not a directory: {folder}")
        return
    
    # Find relevant files in the folder
    simulation_files = list(folder_path.glob("*simulationCDF*"))
    pyramis_files = list(folder_path.glob("*pyramisCDF*"))
    
    # Create list of all file paths
    filenames = []
    
    # Add GT file first if it exists
    if gt_file_path.exists() and gt_file_path.is_file():
        filenames.append(str(gt_file_path))
    else:
        print(f"Warning: GT file not found at {gt_path}")
    
    # Add simulation and pyramis files (convert Path objects to strings)
    filenames.extend([str(f) for f in simulation_files])
    filenames.extend([str(f) for f in pyramis_files])
    
    # Sort filenames for consistent ordering
    filenames.sort(key=lambda x: os.path.basename(x))
    
    # Check if we have any files to process
    if not filenames:
        print(f"No relevant data files found in folder: {folder}")
        return
    
    print(f"Found {len(filenames)} files to process:")
    for filename in filenames:
        print(f"  - {os.path.basename(filename)}")
    
    # Initialize plot with same configuration as original
    curves = []
    
    plt.figure(figsize=(10, 6))
    
    plt.rcParams.update({'font.size': 22})
    plt.rc('axes', labelsize=22)
    plt.rc('legend', fontsize=20)
    plt.rc('xtick', labelsize=20)
    plt.rc('ytick', labelsize=20)
    
    global_max_x = 0
    
    # First pass: find global maximum x value (same as original structure)
    print("First pass: calculating global maximum x value...")
    
    for filename in filenames:
        if os.path.isfile(filename):
            try:
                df = load_cdf_data(filename)
                if df is not None and 'x' in df.columns:
                    max_x = df['x'].max()
                    global_max_x = max(global_max_x, max_x)
                    print(f"  {os.path.basename(filename)}: max_x = {max_x}")
            except Exception as e:
                print(f"Warning: Could not process file {filename}: {e}")
                continue
    
    print(f"Global max x: {global_max_x}")
    
    # Second pass: plotting (same structure as original)
    print("Second pass: plotting curves...")
    
    for filename in filenames:
        if os.path.isfile(filename):
            try:
                df = load_cdf_data(filename)
                if df is not None and 'x' in df.columns and 'y' in df.columns:
                    base_name = os.path.basename(filename)
                    style = get_curve_style(base_name)
                    label = get_curve_label(base_name)
                    
                    curve = plt.plot(df['x'], df['y'], label=label, **style)
                    curves.append((curve[0], label))
                    
                    print(f"  Plotted: {label} ({len(df)} points)")
                    
            except Exception as e:
                print(f"Warning: Could not plot file {filename}: {e}")
                continue
    
    # Check if we have curves to plot
    if not curves:
        print("No curves could be successfully plotted")
        plt.close()
        return
    
    # Set plot parameters (same as original)
    plt.xlim(0, global_max_x * 1.05)
    plt.xlabel('time')
    plt.ylabel('response time distribution')
    plt.ylim(0, 1)
    
    # Add legend
    handles, labels = zip(*curves)
    plt.legend(handles, labels, loc='lower right')
    
    plt.grid(True)
    plt.tight_layout()
    
    # Generate output filename
    folder_name = folder_path.name
    num_files = len(curves)
    
    if num_files == 2:
        output_prefix = "CDF_comparison_2files"
    else:
        output_prefix = f"CDF_comparison_{num_files}files"
    
    if folder_name and folder_name != '.':
        output_filename = f"{output_prefix}_{folder_name}"
    else:
        output_filename = output_prefix
    
    # Save both PNG and PDF (same as original)
    png_path = folder_path / f"{output_filename}.png"
    pdf_path = folder_path / f"{output_filename}.pdf"
    
    plt.savefig(png_path)
    plt.savefig(pdf_path)
    
    print(f"\nPlot saved as:")
    print(f"  {png_path}")
    print(f"  {pdf_path}")
    
    plt.close()
    
    print(f"Successfully plotted {num_files} curves!")

def main():
    folder = "/home/leonardo/sources/epew25/pyramis/results/20250822_162207"
    GT_folder = "/home/leonardo/sources/epew25/pyramis/epew25/POSTP_GT-old-baseline/100000GT_PAR2_SEQ1_ALT2"
    
    
    # Simply call the function with provided arguments
    plot_cdfs_from_folder(folder, GT_folder)

if __name__ == "__main__":
    main()






